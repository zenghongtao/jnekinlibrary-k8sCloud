package com.talebase

void mvn() {
    
    color.green('>>>>>>>>>>>> Build java <<<<<<<<<<<<')
    sh "mvn clean package"      
}

void npm() {
    
    color.green('>>>>>>>>>>>> Build javaScript <<<<<<<<<<<<')
    sh "mvn clean package"      
}


