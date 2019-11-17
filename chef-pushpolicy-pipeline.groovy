pipeline {
    agent {
        docker {
            image 'chef/chefdk:latest'
        }
    }

    environment {
		CHEF_LICENSE = 'accept'
	}

    // triggers {
    //     pollSCM 'H/5 * * * *'
    // }

    parameters {
        choice choices: ['DEV', 'QA', 'PROD'], description: '', name: 'POLICY_GROUP'
        choice choices: ['petstorechef'], description: '', name: 'COOKBOOK'
        choice choices: ['petstore-webserver','petstore-biserver'], description: '', name: 'POLICY_NAME'
        string defaultValue: '-1', description: '', name: 'BUILD_REVISION', trim: true
    }

    options {
      timeout(15)
      timestamps()
      ansiColor('xterm')
      disableConcurrentBuilds()
    }

    stages {
        stage('Pull Artifact'){
            steps {
                rtDownload (
                    serverId: "artifactory-01",
                    spec:
                        """{
                            "files": [
                                {
                                    "pattern": "chef-cookbook/${params.COOKBOOK}/${params.BUILD_REVISION}/${params.POLICY_NAME}*.tgz",
                                    "target": "archives/"
                                }
                            ]
                        }"""
                )
            }
        }
        stage('Push Archive') {
            steps {
                withCredentials([file(credentialsId: 'CHEFAUTOPEM', variable: 'CHEFAUTOPEM'), file(credentialsId: 'KNIFERB', variable: 'KNIFERB')]) {
                    script {
                        for (f in findFiles(glob: "archives/${params.COOKBOOK}/${params.BUILD_REVISION}/${params.POLICY_NAME}*.tgz")) {
                            sh "chef push-archive ${POLICY_GROUP} ${f} --config ${KNIFERB}"
                        }
                    }
                }
            }       
        }
    }
    post {
        always {
            cleanWs()
        }
    }

}