#!/usr/bin/env groovy
import com.talebase.git
import com.talebase.build


void call() {
    def gitlab = new com.talebase.gitlab()
    def jira = new com.talebase.jira()
    def k8s = new com.talebase.kubernetes()

    pipeline {
        agent { node { label "master"}}
    
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'webHookData', 
                 value: '$',
                 expressionType: 'JSONPath',
                 regexpFilter: "",
                 defaultValue: ''
                ],                        
            ],

            genericRequestVariables:[
                [key: 'projectKey']
            ],

            token: 'jira-devops-service',

            causeString: 'Triggered on $webHookData',
            printContributedVariables: false,
            printPostContent: false,
            silentResponse: false
        )
    }    
        stages{
        
            stage("FileterData"){
                steps{
                    script{
                        response = readJSON text: """${webHookData}"""
    
                        println(response)
    
                        env.eventType = response["webhookEvent"]
    
                        switch(eventType) {
                            case "jira:version_created":
                                env.versionName = response["version"]["name"]
                                currentBuild.description = " Trigger by  ${eventType} ${versionName} "
                                break
    
                            case "jira:issue_created":
                                env.issue_id = response['issue']['id']
                                env.issueName = response['issue']['key']
                                env.userName = response['user']['name']
                                env.moduleNames = response['issue']['fields']['components']
                                env.fixVersion = response['issue']['fields']['fixVersions']
                                currentBuild.description = " Trigger by ${userName} ${eventType} ${issueName} "
                                break
    
                            case "jira:issue_updated":
                                env.issue_id = response['issue']['id']
                                env.issueName = response['issue']['key']
                                env.userName = response['user']['name']
                                env.moduleNames = response['issue']['fields']['components']
                                env.fixVersion = response['issue']['fields']['fixVersions']
                                env.statu = response['issue']['fields']['status']['name']
                                currentBuild.description = " Trigger by ${userName} ${eventType} ${issueName} "
                                break
                                
                            case "jira:version_released":
                                env.versionName = response["version"]["name"]
                                currentBuild.description = " Trigger by  ${eventType} ${versionName} "
                                break
    
                            default:
                                println("hello")
                        }
                    }
                }
            }
            
            stage("CreateVersionFile"){
                when {
                    environment name: 'eventType', value: 'jira:version_created' 
                }
                
                steps{
                    script{
                        //获取K8s文件
                        response = k8s.GetDeployment("demo-uat","demoapp")
                        response = response.content
                        //文件转换
                        base64Content = response.bytes.encodeBase64().toString()
                       //上传文件
                       gitlab.CreateRepoFile(7,"demo-uat%2f${versionName}-uat.yaml",base64Content)
                    }
                
                }
            }
            
            stage("DeleteBranch"){
                when {
                    environment name: 'eventType', value: 'jira:version_released'   
                }
                
                steps{
                    script{
                        //获取issuesName
                        println("project%20%3D%20${projectKey}%20AND%20fixVersion%20%3D%20${versionName}%20AND%20issuetype%20%3D%20Task")
                        response = jira.RunJql("project%20%3D%20${projectKey}%20AND%20fixVersion%20%3D%20${versionName}%20AND%20issuetype%20%3D%20Task")
                        
                        response = readJSON text: """${response.content}"""
                        println(response)
                        issues = [:]
                        for ( issue in response['issues']){
                            println(issue["key"])
                            println(issue["fields"]["components"])
                            issues[issue["key"]] = []
                            
                            //获取issue关联的模块
                            for (component in issue["fields"]["components"] ){
                                issues[issue["key"]].add(component["name"])
                            }
                        
                        }
                        
                        println(issues)
                        
                        
                        //搜索gitlab分支是否已合并然后删除
                        
                        
                        for (issue in issues.keySet()){
                            for (projectName in issues[issue]){
                                repoName = projectName.split("-")[0]
                                projectId = gitlab.GetProjectID(repoName, projectName)
                                
                                try {
                                    println("创建合并请求  release-${versionName}  ---> master")
                                    result = gitlab.CreateMr(projectId,"release-${versionName}","master","release-${versionName}--->master")
                                    result = readJSON text: """${result}"""
                                    mergeId = result["iid"]
                                    gitlab.AcceptMr(projectId,mergeId)
                                    
                                    sleep 15
                                } catch(e){
                                    println(e)
                                }
                                response = gitlab.SearchProjectBranches(projectId,issue)
                                
                                println(response[projectId][0]['merged'])
                                
                                if (response[projectId][0]['merged'] == false){
                                    println("${projectName} --> ${issue} -->此分支未合并暂时忽略！")
                                } else {
                                    println("${projectName} --> ${issue} -->此分支已合并准备清理！")
                                    gitlab.DeleteBranch(projectId,issue)
                                }
                            
                            }
    
                        }
                    }
                }
            }
    
            stage("CreateBranchOrMR"){
            
                when {
                    anyOf {
                        environment name: 'eventType', value: 'jira:issue_created'   //issue 创建 /更新
                        environment name: 'eventType', value: 'jira:issue_updated' 
                    }
                }
    
                steps{
                    script{
                        def projectIds = []
                        println(issueName)
                        fixVersion = readJSON text: """${fixVersion}"""
                        println(fixVersion.size())
    
                        //获取项目Id
                        def projects = readJSON text: """${moduleNames}"""
                        for ( project in projects){
                            println(project["name"])
                            projectName = project["name"]
                            currentBuild.description += "\n project: ${projectName}"
                            repoName = projectName.split("-")[0]
                            
                            try {
                                projectId = gitlab.GetProjectID(repoName, projectName)
                                println(projectId)
                                projectIds.add(projectId)   
                            } catch(e){
                                println(e)
                                currentBuild.description += "\n 未获取到项目ID，请检查模块名称！"
                                println("未获取到项目ID，请检查模块名称！")
                            }
                        } 
    
                        println(projectIds)  
    

                        // 获取master short_id
                        for (ID in projectIds){

                            def commitRes = gitlab.GetCommits(ID)
                            def commitsInfo = readJSON text: """${commitRes}"""
                            def short_id = commitsInfo["short_id"] 
                            println("获取当前 master short_id: ${short_id}")

                        }


                        if (fixVersion.size() == 0 && moduleNames != []) {
                            for (id in projectIds){

                                println("新建特性分支--> ${id} --> ${issueName}")
                                currentBuild.description += "\n ${issueName}"
                                gitlab.CreateBranch(id,"master","${issueName}")


                                println("新建比较分支--> ${id} --> compare-${short_id}-${issue_id}")
                                currentBuild.description += "\n compare-${short_id}-${issue_id}"
                                gitlab.CreateBranch(id,"master","compare-${short_id}-${issue_id}")
                          
                            }
                                
                            
    
                        } else if (fixVersion.size() != 0 && moduleNames != [] && statu != '完成') {

                            //获取所有分支信息
                            def branchesRes = gitlab.SearchBranches(id)
                            def branches = readJSON text: """${branchesRes}"""
                            

                            //遍历分支，获取compareBranch
                            def branchesName = []
                            branchesName = branches["name"]
                            for (branchName in branchesName){
                                if (branchName.endsWith("${issue_id}")){
                                    def compareBranch = branchName
                                    println(compareBranch)
                                }                                        
                            }

                            //获取比较分支的 short_id
                            def short_id_compare = compareBranch.split["-"][1]
                            println("比较分支的short_id: ${short_id}")


                            if ("${short_id}" == "${short_id_compare}"){
                                println("到目前为止，无 feature/hostfix 合入 master")
                                fixVersion = fixVersion[0]['name']
                                println("Issue关联release操作,创建合并请求")
                                currentBuild.description += "\n MR release-${fixVersion} to stag-${fixVersion}" 


                                for (id in projectIds){
                            
                                    println("创建release-->${id} -->${fixVersion}分支")
                                    gitlab.CreateBranch(id,"master","release-${fixVersion}")
    
                                    
                                    println("创建合并请求 ${issueName} ---> release-${fixVersion}")
                                    gitlab.CreateMr(id,"${issueName}","release-${fixVersion}","${issueName}--->release-${fixVersion}")
                                
                                }
                            }

                        } else if (fixVersion.size() != 0 && moduleNames != [] && statu == '完成'){

                            fixVersion = fixVersion[0]['name']
                            println("测试点击Issue按钮")
                            currentBuild.description += "\n MR release-${fixVersion} to stag-${fixVersion}"


                            for (id in projectIds){
                            
                                println("创建stag-->${id} -->${fixVersion}分支")
                                gitlab.CreateBranch(id,"master","stag-${fixVersion}")
    
    
                                
                                println("创建合并请求 release-${fixVersion} ---> stag-${fixVersion}")
                                gitlab.CreateMr(id,"release-${fixVersion}","stag-${fixVersion}","release-${fixVersion}--->stag-${fixVersion}")
                                
                            }
                        }
                    }                    
                }
            }
        }
    }
}

