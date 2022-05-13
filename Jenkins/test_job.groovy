pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    parameters { 
        booleanParam(name: 'REMOVE_NGINX', defaultValue: false, description: 'Do you want to remove Nginx?')
        string(name: 'BRANCH', defaultValue: 'develop', description: '')
    }
    stages {
        stage('Clone Git-repo') {
            steps {
                script {
                    cleanWs()
                    sh "git clone -b $BRANCH https://github.com/romchegue/tests.git"
                    sh "ls -l tests"
                }
            }
        }
        stage('Ansible') {
            steps {
                script {
                    dir('tests') {
                        sh "pwd"
                        sh "id"
                        withCredentials([usernamePassword(credentialsId: 'ansible_user', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            ansiblePlaybook(
                                inventory: 'Ansible/inventories/test_hosts',
                                playbook: 'Ansible/playbooks/playbook.yml',
                                extras: '-e "ansible_user=$USERNAME' +
                                        ' ansible_password=$PASSWORD' +
                                        ' ansible_become_password=$PASSWORD' +
                                        ' remove_nginx_package=$REMOVE_NGINX" -vvv',
                                disableHostKeyChecking: true,
                                colorized: true
                            )
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
