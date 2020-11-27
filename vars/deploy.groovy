#!/usr/bin/env groovy

void call() {
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
        # node
        - name: jnlp-agent-node
          image: registry.cn-hangzhou.aliyuncs.com/zenghongtao/jnlp-agent-node:15.3.0
          imagePullPolicy: IfNotPresent
          command:
            - cat
          tty: true
    """
            }
        }
        stages {
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
            stage("node") {
                steps {
                    container("jnlp-agent-node") {
                        script {
                            sh """
                                node -v
                                npm -v
                                sleep 15s
                            """
                        }
                    }
                }
            }
        }
    }

}