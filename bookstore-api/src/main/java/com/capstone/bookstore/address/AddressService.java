package com.capstone.bookstore.address;

import com.capstone.bookstore.address.AddressDtos.AddressRequest;
import com.capstone.bookstore.address.AddressDtos.AddressResponse;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addresses;
    private final UserRepository users;

    public AddressService(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
                .map(AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Address getOwned(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address", addressId));
    }

    public AddressResponse create(Long userId, AddressRequest req) {
        Address a = new Address();
        a.setUser(users.getReferenceById(userId));
        apply(a, req);
        // The first address always becomes the default.
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) || addresses.countByUserId(userId) == 0;
        a.setDefaultAddress(makeDefault);
        addresses.saveAndFlush(a);
        if (makeDefault) {
            addresses.clearDefaultExcept(userId, a.getId());
        }
        return AddressResponse.from(a);
    }

    public AddressResponse update(Long userId, Long addressId, AddressRequest req) {
        Address a = getOwned(userId, addressId);
        apply(a, req);
        if (Boolean.TRUE.equals(req.isDefault())) {
            a.setDefaultAddress(true);
        }
        addresses.saveAndFlush(a);
        if (a.isDefaultAddress()) {
            addresses.clearDefaultExcept(userId, a.getId());
        }
        return AddressResponse.from(getOwned(userId, addressId));
    }

    public void delete(Long userId, Long addressId) {
        Address a = getOwned(userId, addressId);
        boolean wasDefault = a.isDefaultAddress();
        addresses.delete(a);
        addresses.flush();
        if (wasDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescIdAsc(userId).stream().findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private void apply(Address a, AddressRequest req) {
        a.setLabel(req.label());
        a.setRecipientName(req.recipientName().trim());
        a.setLine1(req.line1().trim());
        a.setLine2(req.line2());
        a.setCity(req.city().trim());
        a.setState(req.state());
        a.setPostalCode(req.postalCode().trim());
        a.setCountry(req.country().trim());
        a.setPhone(req.phone());
    }
}
