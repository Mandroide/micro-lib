package com.micro.pattern.command;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CommandEvent {
    Class<? extends Command> command() default Command.class;
}
