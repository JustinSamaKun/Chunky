package org.popcraft.chunky.command;

import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.Input;

import java.util.List;

public class SkipCommand implements ChunkyCommand {
    private final Chunky chunky;

    public SkipCommand(final Chunky chunky) {
        this.chunky = chunky;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        arguments.next().flatMap(Input::tryIntegerSuffixed)
                .ifPresentOrElse(skip -> {
                    var skippedChunks = (long) Math.pow(skip >> 3, 2);
                    chunky.getSelection().skip(skippedChunks);
                }, () -> {});
    }

    @Override
    public List<String> suggestions(final CommandArguments arguments) {
        return List.of();
    }
}
