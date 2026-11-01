package git.artdeell.dnbootstrap.input.model;

public class InputConfiguration {
    public boolean sticky;
    public boolean movesCursor;

    public InputConfiguration() {}

    public InputConfiguration(InputConfiguration inputConfiguration) {
        this.movesCursor = inputConfiguration.movesCursor;
        this.sticky = inputConfiguration.sticky;
    }
}
