#!/usr/bin/env groovy
import com.talebase.git
import com.talebase.build

void call() {
    Object git = new git()
    Object build = new build()

    pipeline{
        agent {
            kubernetes {
                cloud "kubernetes"
                yaml """
    apiVersion: v1
    kind: Pod
    metadata:
      namespace: devops
    spec:
      imagePullSecrets:
        - name: aliyun-image
      containers:
        # maven
        - name: jnlp-agent-maven
          image: registry.cn-hangzhou.aliyuncs.com/zenghongtao/jnlp-agent-maven:3.6.3
          imagePullPolicy: IfNotPresent
          command:
            - cat
          tty: true
    """
            }
        }

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'webHookData', 
                 value: '$',
                 expressionType: 'JSONPath',
                 regexpFilter: "",
                 defaultValue: ''
                ],                 
                [key: 'ref', 
                 value: '$.ref',
                 expressionType: 'JSONPath',
                 regexpFilter: "refs/heads/master",
                 defaultValue: ''
                ],               
                [key: 'project', value: '$.project.name'],
                [key: 'userName', value: '$.user_name'],
                [key: 'pinyinName', value: '$.user_username'],                         
            ],

            genericRequestVariables:[
                [key: 'runOpts']
            ],

            token: 'gitlab-build-service',

            causeString: 'Triggered on $ref',
            printContributedVariables: false,
            printPostContent: false,
            silentResponse: false
        )
    }
        options {
            timestamps()
            disableConcurrentBuilds()
            timeout(time: 1, unit: 'HOURS')
            ansiColor('xterm')
            buildDiscarder(logRotator(numToKeepStr: '5'))
        }

        stages {
            stage("git") {
                steps {
                    script {
                        // git.checkoutBranch()
                        color.green("git code")
                        response = readJSON text: """${webHookData}"""
                        println(response)
                    }
                }
            }

            stage("compile") {
                steps {
                    container("jnlp-agent-maven") {
                        script {
                            color.green("compile")
                        }
                    }
                }
            }

            stage("build images") {
                steps {
                    color.green("build images")
                }
            }

            stage("deploy") {
                steps {
                    print("deploy")
                }
            }
        }
    }

}

