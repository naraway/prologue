package io.naraway.prologue.support.context;

import io.naraway.accent.domain.context.StageContext;
import io.naraway.accent.domain.context.StageRequest;
import org.springframework.core.task.TaskDecorator;

public class StageContextTaskDecorator implements TaskDecorator {
    //
    @Override
    public Runnable decorate(Runnable runnable) {
        //
        StageRequest stageRequest = StageContext.get();
        return () -> {
            try {
                StageContext.set(stageRequest);
                runnable.run();
            } finally {
                StageContext.clear();
            }
        };
    }
}
