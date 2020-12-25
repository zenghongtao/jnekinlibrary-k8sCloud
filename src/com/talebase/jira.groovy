package com.talebase

//封装HTTP请求
def HttpReq(reqType,reqUrl,reqBody){
    def apiServer = "http://10.99.76.225:30085/rest/api/2"

    withCredentials([string(credentialsId: '1af6c2d7-8a28-4a24-a011-f646c643eeac', variable: 'jira-admin-user')]) {   
        result = httpRequest authentication: "${jira-admin-user}",
                httpMode: reqType, 
                contentType: "APPLICATION_JSON",
                consoleLogResponseBody: true,
                ignoreSslErrors: true, 
                requestBody: reqBody,
                url: "${apiServer}/${reqUrl}",
                validResponseCodes: '200:404'
                // quiet: true
    }
    return result
}





//执行JQL
def RunJql(jqlContent){
    apiUrl = "search?jql=${jqlContent}"
    response = HttpReq("GET",apiUrl,'')
    return response
}