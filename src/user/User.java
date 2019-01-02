package user;

import java.io.Serializable;
import java.util.Objects;

public class User implements Serializable {
    private volatile int id;
    private volatile String username;

    public User() {
        this.username = null;
    }

    public User(Integer id, String username) {
        this.id = id;
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) {
            return false;
        }

        User user = (User) o;
        return Objects.equals(this.username, user.username) && Objects.equals(this.id, user.id);
    }

    @Override
    public String toString() {
        return username;
    }
}
