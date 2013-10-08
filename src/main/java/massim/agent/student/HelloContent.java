package massim.agent.student;

import cz.agents.alite.communication.content.Content;

@SuppressWarnings("serial")
public class HelloContent extends Content {
    String content;

    public HelloContent(String content) {
        super(content);
        this.content = content;
    }

    @Override
    public String toString() {
        return "HelloContent [content=" + content + "]";
    }

}
