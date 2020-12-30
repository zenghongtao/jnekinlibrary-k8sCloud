#!/usr/bin/env groovy
import com.talebase.git
import com.talebase.build

void call() {
    Object git = new git()
    Object build = new build()

    pipeline{
        agent any

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
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        stages {
            stage("git") {
                steps {
                    script {
                        println("git code")
                        branch = ref - "refs/heads/"
                        git.checkoutBranch(project,branch)
                    }
                }
            }

            stage("compile") {
                steps {
                    container("jnlp-agent-maven") {
                        script {
                            build.mvn()
                            dir("tds-service/tds-system"){
                                build.dockerBuild()
                            }
                        }
                    }
                }
            }

            stage("deploy") {
                steps {
                    sh """
                        cd /data/tds_kaisa
                        kubectl delete -f tds-system.yaml
                        kubectl apply -f tds-system.yaml
                    """
                }
            }
        }
    }

}

