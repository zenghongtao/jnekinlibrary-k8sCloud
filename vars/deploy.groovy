#!/usr/bin/env groovy
import com.talebase.git

void call() {
    Object git = new git()
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

        options {
            disableConcurrentBuilds()
            timeout(time: 1, unit: 'HOURS')
            ansiColor('xterm')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        stages {
            stage('Git') {
                steps {
                    script {
                        git.checkoutBranch()
                    }
                }
            }

            stage("maven") {
                steps {
                    container("jnlp-agent-maven") {
                        script {
                            sh """
                                mvn -v
                                sleep 15s
                            """
                        }
                    }
                }
            }            
        }
    }

}