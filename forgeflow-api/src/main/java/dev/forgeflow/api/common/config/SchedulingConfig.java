package dev.forgeflow.api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled methods, used by PipelineReaper to sweep stalled pipelines. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
