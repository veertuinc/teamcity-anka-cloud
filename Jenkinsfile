
properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '1')), disableConcurrentBuilds(), disableResume(), copyArtifactPermission('*'), durabilityHint('PERFORMANCE_OPTIMIZED')])

pipeline {
    agent { dockerfile {
        filename 'Dockerfile'
        dir 'shared/docker/orchestrator'
        label 'cloud_container_host'
        reuseNode true
        args "--name ${env.JOB_NAME.replace('%2F', '_').replace('/', '_')}-${env.BUILD_NUMBER}"
    } }
    stages {
        stage('Build Plugin') { steps {
            sh '''
                export PATH="/apache-maven-4.0.0-beta-3/bin:$PATH"
                mvn package
            '''
        } }
        stage('Archive') { steps {
            archiveArtifacts artifacts: 'target/anka-build-cloud-teamcity-plugin-.*.zip', onlyIfSuccessful: true, allowEmptyArchive: true
        } }
    }
}