#!/usr/bin/env groovy
package com.talebase

void checkoutBranche() {
    color.green('>>>>>>>>>>>> Git Pull Config <<<<<<<<<<<<')

    try {      
        checkout([$class: 'GitSCM',
                branches: [[name: "master"]],
                userRemoteConfigs: [[url: "http://gitlab.cepin.com:9999/hongtao.zeng/demo.git", credentialsId: "d23012c1-261d-4a3d-9298-d5108ca8ea2f"]]])
    } catch (e) {
        color.red('>>>>>>>>>>>> Git Pull fail <<<<<<<<<<<<')
        throw e
    }
}