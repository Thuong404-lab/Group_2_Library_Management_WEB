package com.lms.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "Favorites")
public class Favorite {
    @EmbeddedId
    private FavoriteId id;

    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    public Favorite() {
    }

    public Favorite(FavoriteId id, Member member, Book book) {
        this.id = id;
        this.member = member;
        this.book = book;
    }

    public FavoriteId getId() { return id; }
    public void setId(FavoriteId id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
}
