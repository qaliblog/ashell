#!/bin/sh
set -e -u

REPOROOT=$(dirname "$(readlink -f "${0}")")/../

ASHELL_BUILDENV_USER="builder"
ASHELL_BUILDENV_HOME="/home/builder"
: "${ASHELL_BUILDENV_IMAGE_NAME:="xeffyr/native-packages-buildenv"}"
: "${ASHELL_BUILDENV_CONTAINER_NAME:="ashell-buildenv"}"
: "${ASHELL_BUILDENV_RUN_WITH_SUDO:=false}"

if [ "${ASHELL_BUILDENV_RUN_WITH_SUDO}" = "true" ]; then
	SUDO="sudo"
else
	SUDO=
fi

if [ "${GITHUB_EVENT_PATH-x}" != "x" ]; then
	# On CI/CD tty may not be available.
	DOCKER_TTY=""
else
	DOCKER_TTY="--tty"
fi

echo "Running container '${ASHELL_BUILDENV_CONTAINER_NAME}' from image '${ASHELL_BUILDENV_IMAGE_NAME}'..."

$SUDO docker start "${ASHELL_BUILDENV_CONTAINER_NAME}" > /dev/null 2> /dev/null || {
	echo "Creating new container..."

	$SUDO docker run --detach --tty \
		--name "${ASHELL_BUILDENV_CONTAINER_NAME}" \
		--volume "${REPOROOT}:${ASHELL_BUILDENV_HOME}/native-packages" \
		"${ASHELL_BUILDENV_IMAGE_NAME}"

	if [ "$(id -u)" -ne 1000 ] && [ "$(id -u)" -ne 0 ]; then
		echo "Changed builder uid/gid... (this may take a while)"
		$SUDO docker exec ${DOCKER_TTY} "${ASHELL_BUILDENV_CONTAINER_NAME}" \
			sudo chown -Rh "$(id -u):$(id -g)" /data "${ASHELL_BUILDENV_HOME}"
		$SUDO docker exec ${DOCKER_TTY} "${ASHELL_BUILDENV_CONTAINER_NAME}" \
			sudo usermod -u "$(id -u)" "${ASHELL_BUILDENV_USER}"
		$SUDO docker exec ${DOCKER_TTY} "${ASHELL_BUILDENV_CONTAINER_NAME}" \
			sudo groupmod -g "$(id -g)" "${ASHELL_BUILDENV_USER}"
	fi
}

if [ "$#" -eq  "0" ]; then
	$SUDO docker exec --interactive ${DOCKER_TTY} \
		--user "${ASHELL_BUILDENV_USER}" \
		"${ASHELL_BUILDENV_CONTAINER_NAME}" /bin/bash
else
	$SUDO docker exec --interactive ${DOCKER_TTY} \
		--user "${ASHELL_BUILDENV_USER}" \
		"${ASHELL_BUILDENV_CONTAINER_NAME}" "${@}"
fi
