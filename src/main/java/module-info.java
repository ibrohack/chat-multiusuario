module com.psp {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.psp to javafx.fxml;
    exports com.psp;
}
