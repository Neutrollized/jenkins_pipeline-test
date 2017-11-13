node {
    try {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
            echo "\u27A1 inside the try"
    
            stage ('Stage 1') {
                echo 'Hello World 1'
            }
    
            stage ('Stage 2') {
                echo 'Hello World 2'
            }
        }
    }
    catch (err) {
        echo "\u27A1 Caught: ${err}"
    }
    
}
