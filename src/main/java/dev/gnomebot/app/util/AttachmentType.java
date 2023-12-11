package dev.gnomebot.app.util;

import dev.gnomebot.app.App;
import discord4j.core.object.entity.Attachment;

import java.util.regex.Pattern;

public enum AttachmentType {
	FILE,
	TEXT,
	ZIP,
	JAR,
	IMAGE,
	VIDEO;

	public static final Pattern TEXT_REGEX = Pattern.compile("\\.(?:log|txt|toml|json|java|js|php|lua|ts|zs|obj|csv|snbt|bat|cfg|md|html|diff|patch|css|pom|xml|gradle|properties|mcmeta|pdf)$");
	public static final Pattern IMAGE_REGEX = Pattern.compile("\\.(?:gif|jpe?g|tiff?|png|webp|bmp)$");
	public static final Pattern VIDEO_REGEX = Pattern.compile("\\.(?:mov|avi|wmv|flv|3gp|mp4|mpg|mkv)$");
	public static final Pattern ATTACHMENT_PATTERN = Pattern.compile("https?://(?:cdn|media)\\.(?:discord|discordapp)\\.(?:com|net)/attachments/\\d+/\\d+/.*", Pattern.MULTILINE);
	public static final Pattern FULL_IMAGE_PATTERN = Pattern.compile(ATTACHMENT_PATTERN + "\\.(?:gif|jpe?g|tiff?|png|webp|bmp)", Pattern.MULTILINE);
	public static final Pattern FULL_VIDEO_PATTERN = Pattern.compile(ATTACHMENT_PATTERN + "\\.(?:mov|avi|wmv|flv|3gp|mp4|mpg|mkv)", Pattern.MULTILINE);

	public static AttachmentType get(Attachment a) {
		return get(a.getFilename(), a.getContentType().orElse(""));
	}

	public static AttachmentType get(String filename, String contentType) {
		if (!contentType.isEmpty()) {
			if (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed")) {
				return ZIP;
			} else if (contentType.equals("application/java-archive")) {
				return JAR;
			} else if (contentType.startsWith("image/")) {
				return IMAGE;
			} else if (contentType.startsWith("video/")) {
				return VIDEO;
			} else if (contentType.startsWith("text/") || contentType.contains("charset=") || contentType.startsWith("application/json") || contentType.startsWith("application/pdf")) {
				return TEXT;
			}
		}

		if (filename.endsWith(".zip")) {
			return ZIP;
		} else if (filename.endsWith(".jar")) {
			return JAR;
		} else if (TEXT_REGEX.matcher(filename).find()) {
			return TEXT;
		} else if (IMAGE_REGEX.matcher(filename).find()) {
			return IMAGE;
		} else if (VIDEO_REGEX.matcher(filename).find()) {
			return VIDEO;
		}

		if (!contentType.isEmpty()) {
			App.error("Unknown content type: " + contentType);
		}

		return FILE;
	}
}
