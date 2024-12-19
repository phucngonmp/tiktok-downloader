package org.example;

public class VideoInfo {
    private String filename;
    private String link;
    private String view;
    private String like;
    private String comment;
    private String title;
    private String hashtags;
    private String music;

    public VideoInfo(String filename, String link, String view, String like, String comment, String title, String hashtags, String music) {
        this.filename = filename;
        this.link = link;
        this.view = view;
        this.like = like;
        this.comment = comment;
        this.title = title;
        this.hashtags = hashtags;
        this.music = music;
    }

    public String getFilename() {
        return filename;
    }

    public String getLink() {
        return link;
    }

    public String getView() {
        return view;
    }

    public String getLike() {
        return like;
    }

    public String getComment() {
        return comment;
    }

    public String getTitle() {
        return title;
    }

    public String getHashtags() {
        return hashtags;
    }

    public String getMusic() {
        return music;
    }

    @Override
    public String toString() {
        return filename + " | " + link + " | views: " + view + ", likes: " + like + ", comments: " + comment;
    }
}
