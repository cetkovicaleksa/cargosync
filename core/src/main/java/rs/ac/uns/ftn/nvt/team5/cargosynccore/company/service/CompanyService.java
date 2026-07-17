package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto.CompanyRequestForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.Company;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.CompanyRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.repository.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.EmailService;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.CompanyRequest.Status;

@RequiredArgsConstructor
@Service
public class CompanyService {

    private final CompanyRepository companies;
    private final CompanyRequestRepository requests;
    private final CityRepository cities;
    private final UserDetailsService userDetailsService;
    private final ContentStore content;
    private final EmailService email;


    @Transactional(readOnly = true)
    public Company getCompany(UUID id) {
        return companies.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    @Transactional(readOnly = true)
    public List<Company> getCustomerCompanies(UUID customerId) {
        return companies.findByOwner_Id(customerId);
    }

    @Transactional(readOnly = true)
    public CompanyRequest getCompanyRequest(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Company request not found"));
    }

    @Transactional
    public void requestCompany(CompanyRequestForm form) {
        var request = _requestCompany(form);

        var docnames = new ArrayList<String>(request.getImages());
        docnames.addAll(request.getProofs());

        var docs = new ArrayList<>(form.getImages());
        docs.addAll(form.getProofs());

        for (int i = 0; i < docnames.size(); i++) {
            content.save(docnames.get(i), docs.get(i));
        }
//        return request;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected CompanyRequest _requestCompany(CompanyRequestForm form) {
        var city = cities.findByCountryCodeAndCode(form.getCity().getCountryCode(), form.getCity().getCode())
            .orElseThrow(() -> new BadRequestException(
                "Unknown city: " + form.getCity().getCountryCode() + "-" + form.getCity().getCode()
            ));
        var location = new GeoLocation(form.getLocation().getLatitude(), form.getLocation().getLongitude());

        var images = form.getImages().stream()
                .map(img -> "attachments/companies/" + UUID.randomUUID() + "-" + img.getOriginalFilename())
            .collect(Collectors.toCollection(ArrayList::new));
        var proofs = form.getProofs().stream()
                .map(doc -> "attachments/companies/" + UUID.randomUUID() + "-" + doc.getOriginalFilename())
            .collect(Collectors.toCollection(ArrayList::new));

        var request = new CompanyRequest();
        request.setName(form.getName());
        request.setCity(city);
        request.setAddress(form.getAddress());
        request.setLocation(location);
        request.setImages(images);
        request.setProofs(proofs);
        return requests.save(request);
    }

    @Transactional
    public void approveCompanyRequest(UUID id) {
        var approved = _approveCompanyRequest(id);
        var owner = getRequestOwner(approved);
        email.sendCompanyApprovedEmail(owner);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected CompanyRequest _approveCompanyRequest(UUID id) {
        var request = getCompanyRequest(id);

        if (!request.isPending()) {
            throw new BadRequestException("Only pending requests may be approved");
        }

        var owner = getRequestOwner(request);
        request.setStatus(Status.APPROVED);
        requests.flush();

        var company = new Company(
                owner,
                request.getName(),
                request.getCity(),
                request.getAddress(),
                request.getLocation(),
                request.getImages()
        );
        companies.save(company);
        return request;
    }

    @Transactional
    public void rejectCompanyRequest(UUID id, String reason) {
        var rejected = _rejectCompanyRequest(id, reason);
        var owner = getRequestOwner(rejected);
        email.sendCompanyRejectedEmail(owner, reason);
        // maybe delete documents??
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected CompanyRequest _rejectCompanyRequest(UUID id, String reason) {
        var request = getCompanyRequest(id);

        if (!request.isPending()) {
            throw new BadRequestException("Only pending requests may be rejected");
        }

        request.setStatus(Status.REJECTED);
        return request;
    }

    @Transactional(readOnly = true)
    public Page<CompanyRequest> getAllCompanyRequests(Status status, Pageable pageable) {
        return requests.findAllByStatus(status, pageable);
    }

    private Customer getRequestOwner(CompanyRequest request) {
        try {
            var account = (Account) userDetailsService.loadUserByUsername(request.getCreatedBy());
            return (Customer) account.getOwner();
        } catch (ClassCastException e) {
            throw new InternalServerError("Bad request owner", e);
        }
    }

}
