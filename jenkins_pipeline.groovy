//@Library('github.com/Neutrollized/shared-jenkins-library@master')

// docker vars
if (!env.DOCKER_ANGULARCLI) {env.DOCKER_ANGULARCLI = 'neutrollized/chromium-headless-ng'}
if (!env.NG_VER) {env.NG_VER = '1.1.0'}
if (!env.KARMA_PORT) {env.KARMA_PORT = '-p 9876:9876'}
if (!env.PROTRACTOR_PORT) {env.PROTRACTOR_PORT = '-p 49152:49152'}

// node/nmp/ng vars
if (!env.NPM_OPTS) {env.NPM_OPTS = '--silent'}
if (!env.BUILD_OPTS) {env.BUILD_OPTS = '--progress=false'}
if (!env.TEST_OPTS) {env.TEST_OPTS = '--progress=false'}
if (!env.E2E_OPTS) {env.E2E_OPTS = '--progress=false'}

// Mandatory Jenkinsfile vars
//if (!env.BUILD_REPO) {error 'BUILD_REPO must be defined in Jenkinsfile environment.'}

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
                sh 'gcc -Wall test-code/hello.c -o test-code/hello'
            }
        }

        stage ('Compile Go') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                sh 'go build -o ./test-code/hello-world ./test-code/hello-world.go'
            }
        }

	docker.image("${env.DOCKER_ANGULARCLI}:${env.NG_VER}").inside("--privileged ${env.KARMA_PORT} ${env.PROTRACTOR_PORT}") {
	    stage ('Compiling project within docker container') {
		// npm install will install all the dependencies as defined in packages.json
		// under the project directory
	    	sh """
		    cd test-code/angular-realworld-example-app
                    npm install ${env.NPM_OPTS}
		    alias ng="~jenkins/.npm/lib/node_modules/angular-cli/bin/ng"
		    ng --version
		    ng build ${env.BUILD_OPTS}
		"""	
	    }
	    stage ('Karma Unit test') {
		sh "npm install ${env.NPM_OPTS} karma"
		sh "cd test-code/angular-realworld-example-app && ng test --watch=false ${env.TEST_OPTS} --browsers ChromeHeadless"
            }
/*   no need to start ng serve for ng e2e
	    stage ('Start ng serve') {
		sh 'cd test-code/angular-realworld-example-app && ng serve --host 0.0.0.0 --disable-host-check &'
            }
*/
            stage ('Parallel testing within docker container') {
            	parallel "Protractor E2E test": {
                    sh "cd test-code/angular-realworld-example-app && npm install ${env.NPM_OPTS} protractor && ng e2e ${env.E2E_OPTS}"
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
                sh 'test-code/hello'
            },
            "integration test 2": {
                sh 'test-code/hello-world'
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
