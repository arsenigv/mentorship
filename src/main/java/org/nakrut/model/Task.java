package org.nakrut.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 255)
    @Setter
    private String title;

    @Column(columnDefinition = "text")
    @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private TaskStatus status = TaskStatus.TODO;

    @Column(nullable = false)
    @Setter
    private LocalDate dueDate;

    @Formula("""
            CASE status
                WHEN 'TODO' THEN 0
                WHEN 'IN_PROGRESS' THEN 1
                WHEN 'DONE' THEN 2
                ELSE 3
            END
            """)
    private Integer statusSortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Task(String title, String description, LocalDate dueDate, Category category, User user) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.category = category;
        this.user = user;
    }
}
