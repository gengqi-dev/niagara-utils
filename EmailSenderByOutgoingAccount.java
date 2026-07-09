public void onSend() throws Exception {
  Boolean flag = getSendingFlag();
  if (flag == false) {
    return;
  }
  // create email and set to/from
  BEmail email = new BEmail();
  email.setTo(getTo());
  email.setSubject(getSubject());
  email.setBody(new BTextPart(getBody()));

  // add text attachment

  // lookup account and send
  BObject obj = resolveEmailAccount(getEmailAccount());
  if (!(obj instanceof BOutgoingAccount)) {
    throw new IllegalStateException("emailAccount does not resolve to a BOutgoingAccount: " + getEmailAccount());
  }

  BOutgoingAccount account = (BOutgoingAccount) obj;
  account.send(email);
}

private BObject resolveEmailAccount(String emailAccount) throws Exception {
  String accountOrd = emailAccount.trim();
  if (accountOrd.indexOf(':') < 0) {
    accountOrd = "station:|slot:/Services/EmailService/" + accountOrd;
  }

  return BOrd.make(accountOrd).resolve().get();
}
