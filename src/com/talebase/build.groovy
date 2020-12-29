package com.talebase

void mvn() {
    
    color.green('>>>>>>>>>>>> Build java <<<<<<<<<<<<')
    sh "mvn clean package"      
}

void dockerBuild(){
    
    color.green('>>>>>>>>>>>> Dokcer Build <<<<<<<<<<<<')
    sh 'mvn docker:build'
}


