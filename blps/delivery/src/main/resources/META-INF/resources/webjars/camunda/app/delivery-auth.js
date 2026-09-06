const camundaPathIndex = window.location.pathname.indexOf('/camunda/app/');
const deliveryContextPath = camundaPathIndex >= 0
  ? window.location.pathname.substring(0, camundaPathIndex)
  : '';
let deliveryAuthRequestPending = false;

async function synchronizeDeliveryAuthentication() {
  if (deliveryAuthRequestPending) {
    return;
  }
  deliveryAuthRequestPending = true;
  try {
    if (window.location.hash.startsWith('#/login')) {
      await fetch(`${deliveryContextPath}/api/auth/logout`, {
        method: 'POST',
        credentials: 'same-origin'
      });
      return;
    }

    await fetch(`${deliveryContextPath}/api/auth/camunda-token`, {
      method: 'POST',
      credentials: 'same-origin'
    });
  } finally {
    deliveryAuthRequestPending = false;
  }
}

window.addEventListener('hashchange', synchronizeDeliveryAuthentication);
synchronizeDeliveryAuthentication();
