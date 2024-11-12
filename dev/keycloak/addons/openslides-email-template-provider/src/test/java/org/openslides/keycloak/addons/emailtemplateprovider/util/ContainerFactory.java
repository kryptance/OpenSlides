package org.openslides.keycloak.addons.emailtemplateprovider.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.ImagePullPolicy;

import java.util.stream.Collectors;

public class ContainerFactory {

    public static GenericContainer<?> createContainer(String name, DockerCompose.Service serviceConfig) {
        try (GenericContainer<?> container = new GenericContainer<>(serviceConfig.image)) {
            container.withNetworkAliases(name);

            container.withEnv(serviceConfig.environmentMap);

            for (DockerCompose.PortMapping portMapping : serviceConfig.portMappings) {
                container.addExposedPort(portMapping.containerPort);
            }

            container.setPortBindings(serviceConfig.portMappings.stream()
                    .map(DockerCompose.PortMapping::toString)
                    .collect(Collectors.toList()));

            for (DockerCompose.VolumeMapping volumeMapping : serviceConfig.volumeMappings) {
                container.withFileSystemBind(volumeMapping.hostPath, volumeMapping.containerPath);
            }

            if (serviceConfig.command != null) {
                container.withCommand(serviceConfig.commandString);
            }

            return container;
        }
    }
}
