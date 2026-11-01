package git.artdeell.dnbootstrap.input;

import androidx.annotation.NonNull;

import git.artdeell.dnbootstrap.input.model.ViewCreator;

public interface Recreatable {
    @NonNull
    ViewCreator getCreator();
}
