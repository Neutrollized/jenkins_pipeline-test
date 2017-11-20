//@Library('github.com/Neutrollized/shared-jenkins-library@master')

// docker vars
if (!env.DOCKER_NODEJS) {env.DOCKER_NODEJS = 'neutrollized/headless-chrome-nodejs'}
if (!env.NODEJS_VER) {env.NODEJS_VER = '6.11.2'}
if (!env.KARMA_PORT) {env.KARMA_PORT = '-p 9876:9876'}
if (!env.PROTRACTOR_PORT) {env.PROTRACTOR_PORT = '-p 49152:49152'}

// node/nmp/ng vars
if (!env.NPM_OPTS) {env.NPM_OPTS = '--silent'}
if (!env.ANGULAR_CLI) {env.ANGULAR_CLI = '@angular/cli'}
if (!env.NG_PATH) {env.NG_PATH = "node_modules/${env.ANGULAR_CLI}/bin"}
if (!env.BUILD_OPTS) {env.BUILD_OPTS = '--progress=false'}
if (!env.TEST_OPTS) {env.TEST_OPTS = '--progress=false'}
if (!env.E2E_OPTS) {env.E2E_OPTS = '--progress=false'}

// Mandatory Jenkinsfile vars
if (!env.PROJECT_REPO) {error 'PROJECT_REPO must be defined in Jenkinsfile environment.'}
if (!env.PROJECT_DIR) {error 'PROJECT_DIR must be defined in Jenkinsfile environment.'}

node('docker') {
    try {
        // requires AnsiColor plugin
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
            echo "\u27A1 Begins here..."
            stage ('Clean workspace') {
                deleteDir()
                sh 'ls -lah'
            }

            stage ('Checkout source') {
                // Pull the latest commit of the specified branch(es)
                // of the specified submodule(s)
                checkout scm
                sh 'git submodule update --remote --init'
            }  
        }

        stage ('Compile C') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                sh "gcc -Wall ${env.PROJECT_REPO}/hello.c -o ${env.PROJECT_REPO}/hello"
            }
        }

        stage ('Compile Go') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                sh "go build -o ${env.PROJECT_REPO}/hello-world ${env.PROJECT_REPO}/hello-world.go"
            }
        }

	docker.image("${env.DOCKER_NODEJS}:${env.NODEJS_VER}").inside("--privileged ${env.KARMA_PORT} ${env.PROTRACTOR_PORT}") {
	    stage ('Compiling project within docker container') {
		// npm install will install all the dependencies as defined in packages.json
		// under the project directory
		// ng gets installed under node_modules/, so path to ng binary need to be explicit
		// github.com/angular/angular-cli/issues/503
	    	sh """
		    cd ${env.PROJECT_REPO}/${env.PROJECT_DIR}
                    npm install ${env.NPM_OPTS}
		    ${env.NG_PATH}/ng --version
		    ${env.NG_PATH}/ng build ${env.BUILD_OPTS}
		"""	
	    }
	    stage ('Karma Unit test') {
		sh "npm install ${env.NPM_OPTS} karma"
		sh """
		    cd ${env.PROJECT_REPO}/${env.PROJECT_DIR}
		    ${env.NG_PATH}/ng test --watch=false ${env.TEST_OPTS} --browsers ChromeHeadless
		"""
            }
/*   no need to start ng serve for ng e2e
	    stage ('Start ng serve') {
		sh 'cd test-code/angular-realworld-example-app && ng serve --host 0.0.0.0 --disable-host-check &'
            }
*/
            stage ('Parallel testing within docker container') {
            	parallel "Protractor E2E test": {
                    sh """
		        cd ${env.PROJECT_REPO}/${env.PROJECT_DIR}
			npm install ${env.NPM_OPTS} protractor
			${env.NG_PATH}/ng e2e ${env.E2E_OPTS}
		    """
            	},
                "docker test 2": {
                    sh 'date'
            	},
            	"docker test 3": {
		    sh 'hostname'
            	}
            }
	}

        // scripted parallel
        stage ('Parallel testing stage') {
            parallel "integration test 1": {
                sh "${env.PROJECT_REPO}/hello"
            },
            "integration test 2": {
                sh "${env.PROJECT_REPO}/hello-world"
            },
            "ui tests": {
		sh 'echo stuff here'
            }
        }
    }
    catch (err) {
        echo "\u27A1 Caught: ${err}"
    }

}
