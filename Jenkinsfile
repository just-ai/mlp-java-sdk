pipeline {
    options {
        gitLabConnection("gitlab just-ai")
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
    agent {
        label 'caila-dev-cloud-agent'
    }
    stages {
        stage('Versions set') {
            steps {
                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh """mvn versions:set -DnewVersion=${BRANCH_NAME}-SNAPSHOT"""
                }
            }
        }
        stage('Build with maven') {
            steps {
                script {
                    manager.addShortText(env.BRANCH_NAME)
                }

                updateGitlabCommitStatus name: "build", state: "running"

                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh """mvn clean deploy"""
                }
            }
        }
    }
    post {
        failure {
            updateGitlabCommitStatus name: "build", state: "failed"
        }
        success {
            updateGitlabCommitStatus name: "build", state: "success"
        }
        unstable {
            updateGitlabCommitStatus name: "build", state: "failed"
        }
    }
}

