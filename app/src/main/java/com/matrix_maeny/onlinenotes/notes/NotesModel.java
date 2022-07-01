package com.matrix_maeny.onlinenotes.notes;

public class NotesModel {

    private String heading = "";
    private String content = "";

    public NotesModel() {
    }


    public NotesModel(String heading, String content) {
        this.heading = heading;
        this.content = content;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
