#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [version:'']
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def newVersion = ''
    if (config.version == '') {
        newVersion = getNewVersion {}
    } else {
        newVersion = config.version
    }

    def flow = new io.fabric8.Fabric8Commands()

    env.setProperty('VERSION',newVersion)

    kubernetes.image().withName("${env.JOB_NAME}").build().fromPath(".")
    kubernetes.image().withName("${env.JOB_NAME}").tag().inRepository("${config.docker_registry_server}:${config.docker_registry_server_port}/${env.KUBERNETES_NAMESPACE}/${env.JOB_NAME}").withTag(newVersion)

    if (flow.isSingleNode()){
        echo 'Running on a single node, skipping docker push as not needed'
    } else {
        kubernetes.image().withName("${config.docker_registry_server}:${config.docker_registry_server_port}/${env.KUBERNETES_NAMESPACE}/${env.JOB_NAME}").push().withTag(newVersion).toRegistry()
    }

    return newVersion
  }

