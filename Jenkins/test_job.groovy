node {
    timeout(time: 30, unit: 'MINUTES') {
        env.vaultPassFile = "vaultpass.txt"
        env.gitRepoUrl = "https://github.com/romchegue/tests.git"
        currentBuild.result = "SUCCESS"
        try {
            properties([
                parameters([
                    booleanParam(name: 'REMOVE_NGINX', defaultValue: false, description: 'Do you want to remove Nginx?'),
                    string(name: 'BRANCH', defaultValue: 'develop', description: 'Branch for repository with Ansible code: https://github.com/romchegue/tests.git')
                ])
            ])
            stage('Clone Git-repo') {
                cleanWs()
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: BRANCH]],
                    extensions: [],
                    userRemoteConfigs: [[url: env.gitRepoUrl]]
                ])
                sh "ls -la"
            }
            stage('Ansible') {
                prepareVaultPassFile()
                ansiColor('xterm') {
                    ansiblePlaybook(
                        inventory: "${WORKSPACE}/Ansible/inventories/test_hosts",
                        playbook: "${WORKSPACE}/Ansible/playbooks/playbook.yml",
                        extras: "-e \"workspace=${WORKSPACE}"+
                                " remove_nginx_package=$REMOVE_NGINX\" -vv" +
                                " --vault-password-file ${WORKSPACE}/${env.vaultPassFile}",
                        disableHostKeyChecking: true,
                        colorized: true
                    )
                }
            }
        } catch (e) {
            currentBuild.result = "FAILURE"
            echo "[ERROR] Build failed"
            throw e
        } finally {
            cleanWs()
        }
    }
}


private void prepareVaultPassFile() {
    echo "[INFO] Function prepareVaultPassFile()"
    withCredentials([file(credentialsId: 'ansible_vault_pass_file', variable: 'FILE')]) {
        wrap([$class: "MaskPasswordsBuildWrapper"]) {
            sh """
                cat $FILE > ${env.vaultPassFile}
                ls -l ${env.vaultPassFile}
            """
        }
    }
}
