package com.talebase.build

void mvn() {
    
    color.green('>>>>>>>>>>>> Build java <<<<<<<<<<<<')
    sh "mvn clean package"      
}


