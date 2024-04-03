package dev.gnomebot.app;

public class BrainEvent {
	public BrainEventType type;
	public long guild;
	public String content;

	public BrainEvent(BrainEventType type, long guild) {
		this.type = type;
		this.guild = guild;
	}

	public BrainEvent content(String content) {
		this.content = content;
		return this;
	}

	public void post() {
		Brain.ALL.event(this);

		if (guild != 0L) {
			synchronized (Brain.LOCK) {
				Brain.GUILD.computeIfAbsent(guild, Brain::new).event(this);
			}
		}
	}
}
