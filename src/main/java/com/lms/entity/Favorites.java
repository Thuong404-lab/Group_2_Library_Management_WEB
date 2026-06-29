package com.lms.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "Favorites")
public class Favorites {
    @EmbeddedId
    private FavoritesId id;

    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    public Favorites() {
    }

    public Favorites(FavoritesId id, Member member, Book book) {
        this.id = id;
        this.member = member;
        this.book = book;
    }

    public FavoritesId getId() { return id; }
    public void setId(FavoritesId id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
}
