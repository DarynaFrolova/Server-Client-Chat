module com.example.chatlesson7 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chatlesson7 to javafx.fxml;
    exports com.example.chatlesson7;
}