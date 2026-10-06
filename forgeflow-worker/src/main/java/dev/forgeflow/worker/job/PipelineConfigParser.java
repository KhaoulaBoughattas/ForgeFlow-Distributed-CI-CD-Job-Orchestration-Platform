package dev.forgeflow.worker.job;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a repository's .forgeflow.yml, e.g.:
 * <pre>
 * image: node:20-alpine
 * steps:
 *   - name: install
 *     run: npm ci
 *   - name: test
 *     run: npm test
 * </pre>
 * Uses SnakeYAML's SafeConstructor so the pipeline config -- arbitrary, untrusted, repository-
 * supplied YAML -- can never deserialize into anything beyond plain Java maps/lists/scalars.
 */
@Component
public class PipelineConfigParser {

    public record PipelineStep(String name, String run) {
    }

    public record PipelineConfig(String image, List<PipelineStep> steps) {
    }

    public PipelineConfig parse(String yamlContent) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object loaded;
        try {
            loaded = yaml.load(yamlContent);
        } catch (Exception ex) {
            throw new PipelineConfigException("Could not parse .forgeflow.yml as YAML: " + ex.getMessage());
        }

        if (!(loaded instanceof Map<?, ?> root)) {
            throw new PipelineConfigException(".forgeflow.yml must be a YAML mapping at the top level");
        }

        Object imageValue = root.get("image");
        if (!(imageValue instanceof String image) || image.isBlank()) {
            throw new PipelineConfigException(".forgeflow.yml must specify a non-empty 'image'");
        }

        Object stepsValue = root.get("steps");
        if (!(stepsValue instanceof List<?> rawSteps) || rawSteps.isEmpty()) {
            throw new PipelineConfigException(".forgeflow.yml must specify at least one entry under 'steps'");
        }

        List<PipelineStep> steps = new ArrayList<>();
        for (Object rawStep : rawSteps) {
            if (!(rawStep instanceof Map<?, ?> stepMap)) {
                throw new PipelineConfigException("Each entry under 'steps' must be a mapping with 'name' and 'run'");
            }
            Object name = stepMap.get("name");
            Object run = stepMap.get("run");
            if (!(name instanceof String) || !(run instanceof String) || ((String) run).isBlank()) {
                throw new PipelineConfigException("Each step must have a non-empty 'name' and 'run' command");
            }
            steps.add(new PipelineStep((String) name, (String) run));
        }

        return new PipelineConfig(image, steps);
    }
}
