package com.psp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PrimaryController {

    @FXML
    private Button conectar_button;

    @FXML
    private Button desconectar_button;

    @FXML
    private Button enviar_button;

    @FXML
    private Label estado_lbl;

    @FXML
    private TextField ip_tf;

    @FXML
    private TextField mensaje_tf;

    @FXML
    private TextField para_tf;

    @FXML
    private CheckBox privado_checkbox;

    @FXML
    private TextField puerto_tf;

    @FXML
    private TextField usuario_tf;

}
