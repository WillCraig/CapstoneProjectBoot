
package com.northstar.crm.interaction;

import com.northstar.crm.customer.Customer;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "customer_interactions")
public class CustomerInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id", nullable = false, updatable = false)
    private UUID interactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_uuid",
            referencedColumnName = "event_uuid",
            nullable = false
    )
    private Customer eventCustomer;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CustomerInteraction() {
        // Required by JPA.
    }

    public CustomerInteraction(
            Customer customer,
            String channel,
            String summary
    ) {
        this.customer = customer;
        this.eventCustomer = customer;
        this.channel = channel;
        this.summary = summary;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getInteractionId() {
        return interactionId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getChannel() {
        return channel;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void update(String channel, String summary) {
        this.channel = channel;
        this.summary = summary;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
