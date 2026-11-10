/**
 * JavaFX client of the multiuser chat.
 *
 * <p>Offers two user interfaces over the same session: the classic view that
 * mirrors the course screenshot and the advanced view with a user list and
 * conversation tabs.</p>
 */
module com.psp.chat.client {
    requires com.psp.chat.core;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires javafx.graphics;

    opens com.psp.chat.client.view to javafx.fxml;
    exports com.psp.chat.client to javafx.graphics;
}
