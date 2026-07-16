package com.guaneri.securitymaster.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.repository.SecurityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SecurityMasterServiceTest {

  @Mock private SecurityRepository repository;
  @Mock private SecurityEventPublisher eventPublisher;

  private SecurityMasterService service;

  @BeforeEach
  void setUp() {
    service = new SecurityMasterService(repository, eventPublisher);
  }

  @Test
  void createDelegatesToRepositoryAndPublishesCreatedEvent() {
    Security draft = SecurityFixtures.validEquity();
    Security persisted = draft.toBuilder().id(UUID.randomUUID()).build();
    when(repository.insert(draft)).thenReturn(persisted);

    Security result = service.create(draft);

    assertThat(result).isEqualTo(persisted);
    verify(eventPublisher).publish(persisted, SecurityChangeType.CREATED);
  }

  @Test
  void createPropagatesDuplicateIdentifierExceptionWithoutPublishingEvent() {
    Security draft = SecurityFixtures.validEquity();
    when(repository.insert(draft)).thenThrow(new DuplicateIdentifierException("ticker already exists"));

    assertThatThrownBy(() -> service.create(draft)).isInstanceOf(DuplicateIdentifierException.class);

    verifyNoInteractions(eventPublisher);
  }

  static Stream<AssetType> optionTypes() {
    return Stream.of(AssetType.EQUITYOPTION, AssetType.INDEXOPTION, AssetType.FUTUREOPTION);
  }

  @ParameterizedTest
  @MethodSource("optionTypes")
  void createRejectsOptionsMissingStrikePrice(AssetType optionType) {
    Security draft = SecurityFixtures.optionMissingStrike(optionType);

    assertThatThrownBy(() -> service.create(draft)).isInstanceOf(InvalidSecurityFieldsException.class);

    verifyNoInteractions(repository);
    verifyNoInteractions(eventPublisher);
  }

  @ParameterizedTest
  @MethodSource("optionTypes")
  void createAcceptsOptionsWithStrikePrice(AssetType optionType) {
    Security draft = SecurityFixtures.validOption(optionType);
    Security persisted = draft.toBuilder().id(UUID.randomUUID()).build();
    when(repository.insert(draft)).thenReturn(persisted);

    Security result = service.create(draft);

    assertThat(result).isEqualTo(persisted);
  }

  @Test
  void createRejectsBondMissingCouponOrMaturity() {
    Security draft = SecurityFixtures.bondMissingRequiredFields();

    assertThatThrownBy(() -> service.create(draft)).isInstanceOf(InvalidSecurityFieldsException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void createAcceptsValidBond() {
    Security draft = SecurityFixtures.validBond();
    Security persisted = draft.toBuilder().id(UUID.randomUUID()).build();
    when(repository.insert(draft)).thenReturn(persisted);

    assertThat(service.create(draft)).isEqualTo(persisted);
  }

  static Stream<AssetType> swapTypes() {
    return Stream.of(
        AssetType.INTERESTRATESWAP, AssetType.CURRENCYSWAP, AssetType.CREDITDEFAULTSWAP, AssetType.TOTALRETURNSWAP);
  }

  @ParameterizedTest
  @MethodSource("swapTypes")
  void createRejectsSwapsMissingNotionalCurrency(AssetType swapType) {
    Security draft = SecurityFixtures.swapMissingNotionalCurrency(swapType);

    assertThatThrownBy(() -> service.create(draft)).isInstanceOf(InvalidSecurityFieldsException.class);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @MethodSource("swapTypes")
  void createAcceptsValidSwaps(AssetType swapType) {
    Security draft = SecurityFixtures.validSwap(swapType);
    Security persisted = draft.toBuilder().id(UUID.randomUUID()).build();
    when(repository.insert(draft)).thenReturn(persisted);

    assertThat(service.create(draft)).isEqualTo(persisted);
  }

  static Stream<AssetType> fxTypes() {
    return Stream.of(AssetType.FXSPOT, AssetType.FXFORWARD);
  }

  @ParameterizedTest
  @MethodSource("fxTypes")
  void createRejectsFxMissingCurrencies(AssetType fxType) {
    Security draft = SecurityFixtures.fxMissingCurrencies(fxType);

    assertThatThrownBy(() -> service.create(draft)).isInstanceOf(InvalidSecurityFieldsException.class);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @MethodSource("fxTypes")
  void createAcceptsValidFx(AssetType fxType) {
    Security draft = SecurityFixtures.validFx(fxType);
    Security persisted = draft.toBuilder().id(UUID.randomUUID()).build();
    when(repository.insert(draft)).thenReturn(persisted);

    assertThat(service.create(draft)).isEqualTo(persisted);
  }

  @Test
  void updateDelegatesToRepositoryAndPublishesUpdatedEvent() {
    UUID id = UUID.randomUUID();
    Security draft = SecurityFixtures.validEquity().toBuilder().id(id).version(3).build();
    Security persisted = draft.toBuilder().version(4).build();
    when(repository.update(draft)).thenReturn(persisted);

    Security result = service.update(id, draft);

    assertThat(result).isEqualTo(persisted);
    verify(eventPublisher).publish(persisted, SecurityChangeType.UPDATED);
  }

  @Test
  void updatePropagatesNotFoundWithoutPublishingEvent() {
    UUID id = UUID.randomUUID();
    Security draft = SecurityFixtures.validEquity().toBuilder().id(id).version(1).build();
    when(repository.update(draft)).thenThrow(new SecurityNotFoundException(id));

    assertThatThrownBy(() -> service.update(id, draft)).isInstanceOf(SecurityNotFoundException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void updatePropagatesOptimisticLockConflictWithoutPublishingEvent() {
    UUID id = UUID.randomUUID();
    Security draft = SecurityFixtures.validEquity().toBuilder().id(id).version(1).build();
    when(repository.update(draft)).thenThrow(new OptimisticLockException(id, 1, 2));

    assertThatThrownBy(() -> service.update(id, draft)).isInstanceOf(OptimisticLockException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void getByIdReturnsSecurityWhenFound() {
    UUID id = UUID.randomUUID();
    Security existing = SecurityFixtures.validEquity().toBuilder().id(id).build();
    when(repository.findById(id)).thenReturn(Optional.of(existing));

    assertThat(service.getById(id)).isEqualTo(existing);
  }

  @Test
  void getByIdThrowsNotFoundWhenMissing() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(id)).isInstanceOf(SecurityNotFoundException.class);
  }

  @Test
  void lookupBySpecificIdentifierTypeDelegatesToRepository() {
    Security existing = SecurityFixtures.validEquity().toBuilder().id(UUID.randomUUID()).build();
    when(repository.findByIdentifier(IdentifierType.CUSIP, "037833100")).thenReturn(Optional.of(existing));

    assertThat(service.lookup(IdentifierType.CUSIP, "037833100")).isEqualTo(existing);
  }

  @Test
  void lookupThrowsNotFoundWhenNoMatch() {
    when(repository.findByIdentifier(IdentifierType.CUSIP, "nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.lookup(IdentifierType.CUSIP, "nope")).isInstanceOf(SecurityNotFoundException.class);
  }

  @Test
  void universalLookupChecksAllIdentifierColumns() {
    Security existing = SecurityFixtures.validEquity().toBuilder().id(UUID.randomUUID()).build();
    when(repository.findByAnyIdentifier("TEST")).thenReturn(Optional.of(existing));

    assertThat(service.lookupAny("TEST")).isEqualTo(existing);
  }

  @Test
  void universalLookupThrowsNotFoundWhenNoColumnMatches() {
    when(repository.findByAnyIdentifier("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.lookupAny("nope")).isInstanceOf(SecurityNotFoundException.class);
  }

  @Test
  void searchDelegatesToRepository() {
    PagedResult<Security> page = new PagedResult<>(List.of(SecurityFixtures.validEquity()), 0, 20, 1);
    when(repository.search(AssetType.EQUITY, SecurityStatus.ACTIVE, 0, 20)).thenReturn(page);

    assertThat(service.search(AssetType.EQUITY, SecurityStatus.ACTIVE, 0, 20)).isEqualTo(page);
  }

  @Test
  void deactivateSetsStatusInactiveAndPublishesDeactivatedEvent() {
    UUID id = UUID.randomUUID();
    Security existing = SecurityFixtures.validEquity().toBuilder().id(id).status(SecurityStatus.ACTIVE).version(2).build();
    Security deactivated = existing.toBuilder().status(SecurityStatus.INACTIVE).version(3).build();
    when(repository.findById(id)).thenReturn(Optional.of(existing));
    when(repository.update(existing.toBuilder().status(SecurityStatus.INACTIVE).build())).thenReturn(deactivated);

    Security result = service.deactivate(id);

    assertThat(result).isEqualTo(deactivated);
    verify(eventPublisher).publish(deactivated, SecurityChangeType.DEACTIVATED);
  }

  @Test
  void upsertFromFeedUpdatesExistingSecurityMatchedByPaceSecId() {
    Security incoming = Security.builder()
        .paceSecId("PSEC-1")
        .name("Updated Name")
        .currency("USD")
        .status(SecurityStatus.ACTIVE)
        .assetType(AssetType.EQUITY)
        .build();
    Security existing = incoming.toBuilder().id(UUID.randomUUID()).name("Old Name").version(5).build();
    Security merged = incoming.toBuilder().id(existing.id()).version(5).build();
    Security persisted = merged.toBuilder().version(6).build();

    when(repository.findByIdentifier(IdentifierType.PACESEC_ID, "PSEC-1")).thenReturn(Optional.of(existing));
    when(repository.update(merged)).thenReturn(persisted);

    Security result = service.upsertFromFeed(incoming);

    assertThat(result).isEqualTo(persisted);
    verify(repository, never()).insert(any());
    verify(eventPublisher).publish(persisted, SecurityChangeType.UPDATED);
  }

  @Test
  void upsertFromFeedCreatesNewSecurityWhenNoIdentifierMatches() {
    Security incoming = Security.builder()
        .ticker("NEWTICK")
        .name("Brand New")
        .currency("USD")
        .status(SecurityStatus.ACTIVE)
        .assetType(AssetType.EQUITY)
        .build();
    Security persisted = incoming.toBuilder().id(UUID.randomUUID()).build();

    when(repository.findByIdentifier(IdentifierType.TICKER, "NEWTICK")).thenReturn(Optional.empty());
    when(repository.insert(incoming)).thenReturn(persisted);

    Security result = service.upsertFromFeed(incoming);

    assertThat(result).isEqualTo(persisted);
    verify(repository, never()).update(any());
    verify(eventPublisher).publish(persisted, SecurityChangeType.CREATED);
  }
}
