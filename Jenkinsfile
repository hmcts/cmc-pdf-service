#!groovy

//noinspection GroovyAssignabilityCheck Jenkins API requires this format
properties(
  [[$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/cmc-pdf-service/'],
   pipelineTriggers([[$class: 'GitHubPushTrigger']])]
)

@Library('Reform')
import uk.gov.hmcts.Ansible
import uk.gov.hmcts.Packager
import uk.gov.hmcts.Versioner
import uk.gov.hmcts.RPMTagger

Ansible ansible = new Ansible(this, 'cmc')
Packager packager = new Packager(this, 'cmc')
Versioner versioner = new Versioner(this)

milestone()
lock(resource: "pdf-service-${env.BRANCH_NAME}", inversePrecedence: true) {
  node {
    try {
      def version
      def pdfServiceVersion
      String pdfServiceRPMVersion

      stage('Checkout') {
        deleteDir()
        checkout scm
      }

      onMaster {
        stage('Build') {
          versioner.addJavaVersionInfo()
          sh "./gradlew build -x test"
        }

        stage('OWASP dependency check') {
          try {
            sh "./gradlew -DdependencyCheck.failBuild=true dependencyCheck"
          } catch (ignored) {
            archiveArtifacts 'build/reports/dependency-check-report.html'
            notifyBuildResult channel: '#cmc-tech-notification', color: 'warning',
              message: 'OWASP dependency check failed see the report for the errors'
          }
        }

        stage('Test (Unit)') {
          sh "./gradlew test"
        }
      }

      stage('Test (API)') {
        sh "./gradlew apiTest"
      }

      stage('Package (RPM)') {
        sh "./gradlew bootRepackage"
        pdfServiceRPMVersion = packager.javaRPM('', 'pdf-service', 'build/libs/pdf-service-$(./gradlew -q printVersion)-all.jar',
          'springboot', 'src/main/resources/application.yml', true)
        version = "{pdf_service_buildnumber: ${pdfServiceRPMVersion} }"

        onMaster {
          packager.publishJavaRPM('pdf-service')
        }
      }

      stage('Package (Docker)') {
        sh "./gradlew clean installDist"
        pdfServiceVersion = dockerImage imageName: 'cmc/pdf-service-api'
      }

      onMaster {
        stage('Publish Client JAR') {
          def clientVersion = sh returnStdout: true, script: './gradlew -q printClientVersion'
          def clientVersionAlreadyPublished = checkJavaVersionPublished group: 'cmc', artifact: 'pdf-service-client', version: clientVersion

          if (clientVersionAlreadyPublished) {
            print "PDF Service Client version ${clientVersion} is already published, skipping"
          } else {
            def server = Artifactory.server 'artifactory.reform'
            def buildInfo = Artifactory.newBuildInfo()
            def rtGradle = Artifactory.newGradleBuild()
            rtGradle.useWrapper = true
            rtGradle.deployer repo: 'libs-release', server: server
            rtGradle.resolver repo: 'libs-release', server: server

            rtGradle.run rootDir: ".", buildFile: "pdf-service-client/build.gradle", tasks: 'clean assemble', buildInfo: buildInfo

            server.publishBuildInfo buildInfo
          }
        }
      }

      //noinspection GroovyVariableNotAssigned it is guaranteed to be assigned
      RPMTagger rpmTagger = new RPMTagger(this,
        'pdf-service',
        packager.rpmName('pdf-service', pdfServiceRPMVersion),
        'cmc-local'
      )
      onMaster {
        milestone()
        lock(resource: "CMC-deploy-dev", inversePrecedence: true) {
          stage('Deploy (Dev)') {
            ansibleCommitId = ansible.runDeployPlaybook(version, 'dev')
            rpmTagger.tagDeploymentSuccessfulOn('dev')
            rpmTagger.tagAnsibleCommit(ansibleCommitId)
          }
        }

        milestone()
        lock(resource: "CMC-deploy-demo", inversePrecedence: true) {
          stage('Deploy (Demo)') {
            ansible.runDeployPlaybook(version, 'demo')
          }
        }

        milestone()
      }

    } catch (err) {
      onMaster {
        archiveArtifacts 'build/reports/**/*.html'
        archiveArtifacts 'build/pdf-service/reports/**/*.html'
      }
      notifyBuildFailure channel: '#cmc-tech-notification'
      throw err
    }
  }
  milestone()
}
