package com.lms.entity;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FavoritesId implements Serializable {
    private Integer memberId;
    private Integer bookId;

    public FavoritesId() {}
    public FavoritesId(Integer memberId, Integer bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
    }

    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoritesId that = (FavoritesId) o;
        return Objects.equals(memberId, that.memberId) && Objects.equals(bookId, that.bookId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(memberId, bookId);
    }
}
