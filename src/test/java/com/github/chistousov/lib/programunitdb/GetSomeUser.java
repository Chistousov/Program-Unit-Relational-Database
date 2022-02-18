package com.github.chistousov.lib.programunitdb;

import java.time.LocalDateTime;
import java.util.List;

import com.github.chistousov.lib.programunitdb.annotations.Column;
import com.github.chistousov.lib.programunitdb.annotations.OutParam;

public class GetSomeUser {

    @OutParam(name = "admins")
    public class Admin {
        private String name;
        private String comment;
        private LocalDateTime createdate;
        
        public String getName() {
            return name;
        }
        public void setName(@Column(name = "name") String name) {
            this.name = name;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(@Column(name = "comment") String comment) {
            this.comment = comment;
        }
        public LocalDateTime getCreatedate() {
            return createdate;
        }
        public void setCreatedate(@Column(name = "createdate") LocalDateTime createdate) {
            this.createdate = createdate;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;
            result = prime * result + ((comment == null) ? 0 : comment.hashCode());
            result = prime * result + ((createdate == null) ? 0 : createdate.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Admin other = (Admin) obj;

            if (comment == null) {
                if (other.comment != null)
                    return false;
            } else if (!comment.equals(other.comment))
                return false;
            if (createdate == null) {
                if (other.createdate != null)
                    return false;
            } else if (!createdate.equals(other.createdate))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
        
    }

    private List<Admin> admins;
    
    public List<Admin> getAdmins() {
        return admins;
    }
    public void setAdmins(@OutParam(name = "admins") List<Admin> admins) {
        this.admins = admins;
    }

    @OutParam(name = "users")
    public class User {
        private String name;
        private String comment;
        private LocalDateTime createdate;
        
        public String getName() {
            return name;
        }
        public void setName(@Column(name = "name") String name) {
            this.name = name;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(@Column(name = "comment") String comment) {
            this.comment = comment;
        }
        public LocalDateTime getCreatedate() {
            return createdate;
        }
        public void setCreatedate(@Column(name = "createdate") LocalDateTime createdate) {
            this.createdate = createdate;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;
            result = prime * result + ((comment == null) ? 0 : comment.hashCode());
            result = prime * result + ((createdate == null) ? 0 : createdate.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            User other = (User) obj;
            if (comment == null) {
                if (other.comment != null)
                    return false;
            } else if (!comment.equals(other.comment))
                return false;
            if (createdate == null) {
                if (other.createdate != null)
                    return false;
            } else if (!createdate.equals(other.createdate))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
        
    }

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }
    public void setUsers(@OutParam(name = "users") List<User> users) {
        this.users = users;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((admins == null) ? 0 : admins.hashCode());
        result = prime * result + ((users == null) ? 0 : users.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GetSomeUser other = (GetSomeUser) obj;
        if (admins == null) {
            if (other.admins != null)
                return false;
        } else if (!admins.equals(other.admins))
            return false;
        if (users == null) {
            if (other.users != null)
                return false;
        } else if (!users.equals(other.users))
            return false;
        return true;
    }

    

}
