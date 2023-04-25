
properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '1')), disableConcurrentBuilds(), disableResume(), copyArtifactPermission('*'), durabilityHint('PERFORMANCE_OPTIMIZED')])

pipeline {
    agent { node { label 'maven' } }
    stages {
        stage('Build Plugin') { steps {
            sh 'JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn package'
        } }
        stage('Archive') { steps {
            archiveArtifacts artifacts: 'target/anka-build-tc.zip', onlyIfSuccessful: true, allowEmptyArchive: true
        } }
    }
}