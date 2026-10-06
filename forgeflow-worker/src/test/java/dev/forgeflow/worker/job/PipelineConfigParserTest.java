package dev.forgeflow.worker.job;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineConfigParserTest {

    private final PipelineConfigParser parser = new PipelineConfigParser();

    @Test
    void parsesValidConfig() {
        String yaml = """
                image: node:20-alpine
                steps:
                  - name: install
                    run: npm ci
                  - name: test
                    run: npm test
                """;

        PipelineConfigParser.PipelineConfig config = parser.parse(yaml);

        assertThat(config.image()).isEqualTo("node:20-alpine");
        assertThat(config.steps()).containsExactly(
                new PipelineConfigParser.PipelineStep("install", "npm ci"),
                new PipelineConfigParser.PipelineStep("test", "npm test"));
    }

    @Test
    void parsesSingleStepConfig() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - name: build
                    run: echo hello
                """;

        PipelineConfigParser.PipelineConfig config = parser.parse(yaml);

        assertThat(config.image()).isEqualTo("alpine:3.20");
        assertThat(config.steps()).hasSize(1);
        assertThat(config.steps().get(0).name()).isEqualTo("build");
    }

    @Test
    void rejectsInvalidYaml() {
        String yaml = "image: [this is not: valid: yaml: at all";

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("Could not parse");
    }

    @Test
    void rejectsNonMappingTopLevel() {
        String yaml = "- just\n- a\n- list\n";

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("mapping");
    }

    @Test
    void rejectsMissingImage() {
        String yaml = """
                steps:
                  - name: build
                    run: echo hi
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("image");
    }

    @Test
    void rejectsBlankImage() {
        String yaml = """
                image: ""
                steps:
                  - name: build
                    run: echo hi
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("image");
    }

    @Test
    void rejectsMissingSteps() {
        String yaml = "image: alpine:3.20\n";

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("steps");
    }

    @Test
    void rejectsEmptySteps() {
        String yaml = """
                image: alpine:3.20
                steps: []
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("steps");
    }

    @Test
    void rejectsStepMissingName() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - run: echo hi
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsStepMissingRun() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - name: build
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("run");
    }

    @Test
    void rejectsStepWithBlankRun() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - name: build
                    run: ""
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class);
    }

    @Test
    void rejectsStepThatIsNotAMapping() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - just-a-string
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining("mapping");
    }

    @Test
    void preservesStepOrder() {
        String yaml = """
                image: alpine:3.20
                steps:
                  - name: a
                    run: echo 1
                  - name: b
                    run: echo 2
                  - name: c
                    run: echo 3
                """;

        List<PipelineConfigParser.PipelineStep> steps = parser.parse(yaml).steps();

        assertThat(steps).extracting(PipelineConfigParser.PipelineStep::name)
                .containsExactly("a", "b", "c");
    }
}
