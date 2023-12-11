package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.server.AuthLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyDiscordCommand {
	String name();

	String help() default "No information";

	String[] aliases() default {};

	String arguments() default "";

	AuthLevel permissionLevel() default AuthLevel.MEMBER;
}
