#!/bin/bash
#
# ci-publish-apidocs.sh
#
# jpn - 20240308
#


## get job id of build job (required to access artifacts)
RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=running" )"
MAVEN_BUILD_JOB_ID="$( echo "${RESULT}" | jq -c '.[] | select(.name | contains("javadoc")).id' )"
if [ -z "${MAVEN_BUILD_JOB_ID##*[!0-9]*}" ]; then
    echo "ERROR! Could not get job id from 'javadoc' job. (MAVEN_BUILD_JOB_ID=${MAVEN_BUILD_JOB_ID})"
    exit 1
fi


## dmx-platform ?
if [ "${CI_PROJECT_ROOT_NAMESPACE}" != "dmx-platform" ] || [ "${CI_PROJECT_NAME}" != "dmx-platform" ]; then
    echo "ERROR! Invalid CI namespace: CI_PROJECT_ROOT_NAMESPACE/CI_PROJECT_NAME != [dmx-platform]."
    exit 1
fi

## url of cgi-bin with '?'
WEBCGI='https://download.dmx.systems/cgi-bin/v1/deploy-apidocs.cgi?'

## action: call cgi-bin
RESULT="$( wget --server-response -q -O - "${WEBCGI}/${CI_PROJECT_PATH}/-/jobs/${MAVEN_BUILD_JOB_ID}/download" 2>&1 | head -n1 )"
if [ -z "$( echo "${RESULT}" | grep 200 | grep OK )" ]; then
    echo "ERROR! Failed to trigger download for apidocs. (RESULT=${RESULT})"
    exit 1
else
    echo "INFO: Successfuly triggered download for apidocs. (RESULT=${RESULT})"
fi

## EOF
